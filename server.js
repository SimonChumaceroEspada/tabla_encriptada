const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const pool = require("./db"); // Conexión a PostgreSQL

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: { origin: "http://localhost:3000" },
});

// Función para notificar a los clientes
const notifyClients = async () => {
    const data = await pool.query("SELECT * FROM main_table");
    const logs = await pool.query("SELECT * FROM log_table");
    io.emit("updateData", { mainTable: data.rows, logTable: logs.rows });
};

// Conectar clientes al WebSocket
io.on("connection", (socket) => {
    console.log("Cliente conectado:", socket.id);
    socket.on("disconnect", () => console.log("Cliente desconectado:", socket.id));
});

server.listen(3003, () => console.log("Servidor corriendo en http://localhost:3003"));

module.exports = { server, notifyClients };
