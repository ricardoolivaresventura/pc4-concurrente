/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.pc4.concurrente;

/**
 *
 * @author rlov
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.StringTokenizer;

public class NodoDeDireccion {

    private static final int PUERTO_ESCUCHA = 8080;
    private ExecutorService pool;
    private List<Nodo> nodos;
    private List<Socket> clienteSockets;
    private final String NODE_IP = "192.168.56.1";

    public NodoDeDireccion() {
        this.pool = Executors.newFixedThreadPool(10);
        this.nodos = new ArrayList<>(); // Aquí guardamos los nodos
        this.clienteSockets = new ArrayList<>(); // Aquí guardamos todos los sockets de clientes
    }

    public void agregarNodo(Nodo nodo) {
        // Agregamos nodos a la lista de nodos para almacenar toda su información
        nodos.add(nodo);
        System.out.println("Nodo agregado: " + nodo.getDireccionIP() + ":" + nodo.getPuerto());
    }

    private Nodo obtenerNodoMaestro(List<Nodo> nodos) {
        // Implementa lógica para obtener el nodo maestro.
        return nodos.stream()
                .filter(Nodo::esMaestro)
                .findFirst()
                .orElse(null);
    }

    private Nodo determinarNodoEsclavoAlgoritmo(String detallesTransaccion, List<Nodo> nodos) {
        // Implementa el algoritmo para determinar a qué nodo esclavo enviar la solicitud de actualización.
        // Puedes basarte en detallesTransaccion, carga de trabajo de los nodos, etc.
        // Retorna el nodo esclavo seleccionado.
        // Aquí hay un ejemplo simple utilizando el tamaño de la lista de nodos esclavos.
        // int indiceNodoEsclavo = detallesTransaccion.hashCode() % nodos.size();
        int indiceNodoEsclavo = 0;
        return nodos.get(indiceNodoEsclavo);
    }

    public void iniciar() {
        try (ServerSocket servidorSocket = new ServerSocket(PUERTO_ESCUCHA)) {
            System.out.println("Nodo de dirección escuchando en el puerto " + PUERTO_ESCUCHA);

            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                String direccionIp = clienteSocket.getInetAddress().getHostAddress();
                if (direccionIp == NODE_IP) {
                    Nodo nodo = new Nodo(direccionIp, PUERTO_ESCUCHA, true);
                    nodos.add(nodo);
                }
                pool.submit(new ManejadorCliente(clienteSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ManejadorCliente implements Runnable {

        private final Socket clienteSocket;

        public ManejadorCliente(Socket clienteSocket) {
            this.clienteSocket = clienteSocket;
        }

        @Override
        public void run() {
            try (InputStream entrada = clienteSocket.getInputStream();
             OutputStream salida = clienteSocket.getOutputStream(); 
             BufferedReader lector = new BufferedReader(new InputStreamReader(entrada));
            PrintWriter escritor = new PrintWriter(salida, true)) {

                String solicitudCliente = lector.readLine();
                System.out.println("solicitudCliente: " + solicitudCliente);
                // Son 1000 solicitudes por cliente, entonces debemos seprarlos
                // por "," y luego con un bucle de 1000 elementos llamaremos a la función
                // de procesarSolicitud, en la cual se mandará la solicitud de cada iteración
                String[] arrayDeCadenas = solicitudCliente.split(",");
                System.out.println("arrayDeCadenas: " + arrayDeCadenas.length);

                // Lista para almacenar los futuros de las solicitudes
                List<CompletableFuture<String>> futures = new ArrayList<>();

                for (int i = 0; i < arrayDeCadenas.length; i++) {
                    int solicitudIndex = i;
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        // Procesar la solicitud en un hilo separado
                        return procesarSolicitud(arrayDeCadenas[solicitudIndex], nodos);
                    });

                    // Agregar el futuro a la lista
                    futures.add(future);
                }

                // Esperar a que todas las solicitudes se completen
                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                // Obtener el resultado de todas las solicitudes
                allOf.join();

                // Ahora puedes acceder a los resultados individuales
                for (CompletableFuture<String> future : futures) {
                    String respuestaNodo = future.join();
                    System.out.println("respuestaNodo: " + respuestaNodo);
                    // Hacer algo con la respuesta
                    // escritor.println(respuestaNodo);
                }

                /*for (int i = 0; i < arrayDeCadenas.length; i++) {
                    String respuestaNodo = procesarSolicitud(arrayDeCadenas[i], nodos); // Tu lógica aquí
                    // escritor.println(respuestaNodo);
                }*/
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String procesarSolicitud(String solicitud, List<Nodo> nodos) {
            System.out.println("solicitud: " + solicitud);
            StringTokenizer tokenizer = new StringTokenizer(solicitud, "-");

            if (tokenizer.countTokens() < 3) {
                return "Solicitud inválida.";
            }

            String idSolicitud = tokenizer.nextToken();
            String tipoTransaccion = tokenizer.nextToken();
            String detallesTransaccion = tokenizer.nextToken();

            System.out.println("Tipo de transacción: " + tipoTransaccion);
            switch (tipoTransaccion) {
                case "L":
                case "A":
                    // Para solicitudes de lectura, enviar directamente al nodo maestro
                    // Nodo nodoMaestro = obtenerNodoMaestro(nodos);
                    Nodo nodoEsclavo = determinarNodoEsclavoAlgoritmo(detallesTransaccion, nodos);
                    return comunicarseConNodo(nodoEsclavo, tipoTransaccion, detallesTransaccion);
                /*case "A":
                    // Para solicitudes de actualización, utilizar algoritmo para determinar nodo esclavo
                    Nodo nodoEsclavo = determinarNodoEsclavoAlgoritmo(detallesTransaccion, nodos);
                    System.out.println("nodoEsclavo: " + nodoEsclavo);
                    return comunicarseConNodo(nodoEsclavo, tipoTransaccion, detallesTransaccion);*/
                default:
                    return "Tipo de transacción no válido.";
            }
        }

        private String comunicarseConNodo(Nodo nodoDestino, String tipoTransaccion, String detallesTransaccion) {
            try (Socket socketNodo = 
                    new Socket( nodoDestino.getDireccionIP(), 
                            nodoDestino.getPuerto()); 
                    PrintWriter out = new PrintWriter(socketNodo.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socketNodo.getInputStream()))) {

                // Enviar la solicitud al nodo destino
                out.println(tipoTransaccion + "-" + detallesTransaccion);

                // Recibir la respuesta del nodo destino
                String respuestaNodo = in.readLine();

                System.out.println("Respuesta del nodo------: " + respuestaNodo);

                return respuestaNodo;
            } catch (IOException e) {
                System.err.println("Error: " + e);
                return "Error en la comunicación con el nodo.";
            }

        }

    }

    class Nodo {

        private String direccionIP;
        private int puerto;
        private boolean isMaster;

        public Nodo(String direccionIP, int puerto, boolean isMaster) {
            this.direccionIP = direccionIP;
            this.puerto = puerto;
            this.isMaster = isMaster;
        }

        public String getDireccionIP() {
            return direccionIP;
        }

        public int getPuerto() {
            return puerto;
        }

        public boolean esMaestro() {
            return isMaster;
        }

    }

    public static void main(String[] args) {
        NodoDeDireccion nodoDeDireccion = new NodoDeDireccion();
        nodoDeDireccion.iniciar();
    }
}
