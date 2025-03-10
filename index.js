const express = require("express");
const cors = require("cors");
const { server, notifyClients } = require("./server"); // Esto asume que movemos la lógica del servidor

const app = express();
app.use(cors());
app.use(express.json());
app.use("/api", require("./routes")); // Asegúrate de que esta línea esté presente

// Notificar cambios periódicamente
setInterval(notifyClients, 5000); // Ajusta el tiempo según lo necesario

console.log("Servidor corriendo en http://localhost:3003");