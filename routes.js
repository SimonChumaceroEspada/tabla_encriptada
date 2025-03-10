const express = require("express");
const pool = require("./db"); // Conexión a PostgreSQL
const { io } = require("./server"); // Importar io desde server.js
const router = express.Router();

// Emitir un evento cuando hay cambios en la base de datos
const notifyClients = async () => {
    const data = await pool.query("SELECT * FROM main_table");
    const logs = await pool.query("SELECT * FROM log_table");
    io.emit("updateData", { mainTable: data.rows, logTable: logs.rows });
};

// Endpoint para insertar en main_table
router.post("/data", async (req, res) => {
    const { name } = req.body;
    await pool.query("INSERT INTO main_table (name) VALUES ($1)", [name]);
    await notifyClients();
    res.json({ message: "Dato insertado" });
});

// Endpoint para obtener datos
router.get("/data", async (req, res) => {
    const data = await pool.query("SELECT * FROM main_table");
    res.json(data.rows);
});

router.get("/logs", async (req, res) => {
    const logs = await pool.query("SELECT * FROM log_table");
    res.json(logs.rows);
});

module.exports = router;
