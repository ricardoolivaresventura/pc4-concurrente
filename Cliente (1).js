const net = require('net');

// Configuración del cliente
const serverHost = 'localhost';
const serverPort = 8080; // Asegúrate de usar el puerto correcto

// Inicializar el contador de solicitud
let idSolicitudCounter = 1;

// Función para generar transacciones aleatorias
function generateRandomTransaction() {
    const transactionType = Math.random() < 0.6 ? 'L' : 'A'; // 60% lectura, 40% actualización
    const accountId = Math.floor(Math.random() * 1000000) + 1; // ID de cuenta aleatorio

    const id_solicitud = idSolicitudCounter++;

    if (transactionType === 'L') {
        // Lectura
        return `${id_solicitud}-L-${accountId}`;
    } else {
        // Actualización
        const targetAccountId = Math.floor(Math.random() * 1000000) + 1; // ID de cuenta aleatorio para la actualización
        const amount = (Math.random() * 1000).toFixed(2); // Monto aleatorio para actualización con dos decimales

        return `${id_solicitud}-A-${accountId};${targetAccountId};${amount}`;
    }
}

// Conectar al servidor
const client = net.createConnection({ host: serverHost, port: serverPort }, () => {
    console.log('Cliente conectado al servidor');

    // Simular envío de transacciones
    for (let i = 0; i < 1000; i++) {
        const transaction = generateRandomTransaction();
        console.log(`Enviando transacción: ${transaction}`);
        client.write(`${transaction},`);
    }

    // Cerrar la conexión después de enviar transacciones
    client.end();
});

// Manejar eventos de la conexión
client.on('data', (data) => {
    console.log(`Respuesta del servidor: ${data}`);
});

client.on('end', () => {
    console.log('Cliente desconectado del servidor');
});
