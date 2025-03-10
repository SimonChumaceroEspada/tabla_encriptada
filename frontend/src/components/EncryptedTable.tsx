import { useEffect, useState } from "react";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper, TextField, Button } from "@mui/material";
import { io, Socket } from "socket.io-client";
import { api } from "../api";
import CryptoJS from "crypto-js";

const socket: Socket = io("http://localhost:3003");
const SECRET_KEY = "maquina";
// Debe ser segura y privada
interface EncryptedLogTableData {
    id: number;
    main_id: number;
    action: string;
    action_date: string;
}

const EncryptedTable = () => {
    const [logs, setLogs] = useState<EncryptedLogTableData[]>([]);
    const [decryptedLogs, setDecryptedLogs] = useState<EncryptedLogTableData[]>([]);
    const [password, setPassword] = useState("");

    useEffect(() => {
        api.get("/logs").then((res) => {
            const encryptedData = res.data.map((log: EncryptedLogTableData) => ({
                ...log,
                action: CryptoJS.AES.encrypt(log.action, SECRET_KEY).toString(),
                action_date: CryptoJS.AES.encrypt(log.action_date, SECRET_KEY).toString(),
            }));
            setLogs(encryptedData);
        });

        const handleUpdate = (update: { logTable: EncryptedLogTableData[] }) => {
            const encryptedData = update.logTable.map((log) => ({
                ...log,
                action: CryptoJS.AES.encrypt(log.action, SECRET_KEY).toString(),
                action_date: CryptoJS.AES.encrypt(log.action_date, SECRET_KEY).toString(),
            }));
            setLogs(encryptedData);
        };

        socket.on("updateData", handleUpdate);

        return () => {
            socket.off("updateData", handleUpdate);
        };
    }, []);

    const decryptData = () => {
        if (password !== SECRET_KEY) {
            alert("Contraseña incorrecta");
            return;
        }

        const decryptedData = logs.map((log) => ({
            ...log,
            action: CryptoJS.AES.decrypt(log.action, SECRET_KEY).toString(CryptoJS.enc.Utf8),
            action_date: CryptoJS.AES.decrypt(log.action_date, SECRET_KEY).toString(CryptoJS.enc.Utf8),
        }));
        setDecryptedLogs(decryptedData);
    };

    const encryptData = () => {
        const encryptedData = decryptedLogs.map((log) => ({
            ...log,
            action: CryptoJS.AES.encrypt(log.action, SECRET_KEY).toString(),
            action_date: CryptoJS.AES.encrypt(log.action_date, SECRET_KEY).toString(),
        }));
        setLogs(encryptedData);
        setDecryptedLogs([]);
    };

    return (
        <Paper>
            <h2>Encrypted Log Table</h2>
            <TextField
                label="Contraseña"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            <Button onClick={decryptData}>Desencriptar</Button>
            <Button onClick={encryptData}>Encriptar</Button>

            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Main ID</TableCell>
                        <TableCell>Action</TableCell>
                        <TableCell>Date</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {(decryptedLogs.length > 0 ? decryptedLogs : logs).map((log) => (
                        <TableRow key={log.id}>
                            <TableCell>{log.id}</TableCell>
                            <TableCell>{log.main_id}</TableCell>
                            <TableCell>{log.action}</TableCell>
                            <TableCell>{log.action_date}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Paper>
    );
};

export default EncryptedTable;