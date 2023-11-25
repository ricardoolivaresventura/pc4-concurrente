import socket
import random  # Añade esta línea para importar el módulo random# Configuración del nodo esclavo
slave_host = 'localhost'
slave_port = 5000

# Función para conectar con el nodo maestro y realizar la prueba de trabajo
def connect_to_master(word):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as slave_socket:
        slave_socket.connect((slave_host, slave_port))

        # Enviar la palabra al nodo maestro
        slave_socket.sendall(word.encode())

        # Recibir el resultado de la prueba de trabajo
        result = slave_socket.recv(1024).decode()

        print(f"Resultado de la prueba de trabajo: {result}")

if __name__ == "__main__":
    # Palabras que podrían ser seleccionadas por el nodo maestro
    words_to_evaluate = ["hola", "adios", "python", "blockchain"]

    # Seleccionar una palabra para este nodo esclavo
    word = random.choice(words_to_evaluate)
    # Conectar con el nodo maestro y realizar la prueba de trabajo
    connect_to_master(word)
