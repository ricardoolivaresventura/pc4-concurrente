import socket
import threading
import hashlib
import time

# Configuración del servidor maestro
master_host = 'localhost'
master_port = 5000
n_zeros_required = 3  # Número de ceros requeridos en la prueba de trabajo

# Palabras a evaluar (podría ser una lista más grande)
words_to_evaluate = ["hola", "adios", "python", "blockchain"]

# Estructura básica del bloque
class Block:
    def __init__(self, transactions, previous_hash):
        self.transactions = transactions
        self.previous_hash = previous_hash

# Función para realizar la prueba de trabajo
def proof_of_work(word, n_zeros):
    nonce = 0
    while True:
        data = f"{word}{nonce}"
        hash_result = hashlib.sha256(data.encode()).hexdigest()
        if hash_result.startswith('0' * n_zeros):
            return nonce, hash_result
        nonce += 1

# Función para manejar cada conexión con un nodo esclavo
def handle_slave_connection(conn, addr, word):
    print(f"Nodo esclavo conectado desde {addr}")

    # Realizar la prueba de trabajo
    nonce, hash_result = proof_of_work(word, n_zeros_required)

    # Enviar el resultado al nodo esclavo
    conn.sendall(f"{nonce},{hash_result}".encode())
    conn.close()

# Función principal del servidor maestro
def master_server():
    # Inicializar el bloque génesis
    genesis_block = Block(transactions=[], previous_hash="")

    # Inicializar la cadena de bloques con el bloque génesis
    blockchain = [genesis_block]

    # Inicializar el servidor maestro
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as master_socket:
        master_socket.bind((master_host, master_port))
        master_socket.listen()

        print(f"Nodo maestro esperando conexiones en {master_host}:{master_port}")

        while True:
            conn, addr = master_socket.accept()

            # Seleccionar una palabra para el nodo esclavo
            word = words_to_evaluate[len(blockchain) % len(words_to_evaluate)]

            # Crear un hilo para manejar la conexión con el nodo esclavo
            thread = threading.Thread(target=handle_slave_connection, args=(conn, addr, word))
            thread.start()

if __name__ == "__main__":
    master_server()
