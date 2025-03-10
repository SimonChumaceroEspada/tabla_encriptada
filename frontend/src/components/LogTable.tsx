import { useEffect, useState } from "react";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper } from "@mui/material";
import { io, Socket } from "socket.io-client";
import { api } from "../api";

const socket: Socket = io("http://localhost:3003");

interface LogTableData {
    id: number;
    main_id: number;
    action: string;
    action_date: string;
}

interface EncryptedLogTableData {
    id: number;
    main_id: number;
    encrypted_action: string;
    encrypted_action_date: string;
}

const LogTable = () => {
    const [logs, setLogs] = useState<LogTableData[]>([]);
    const [encryptedLogs, setEncryptedLogs] = useState<EncryptedLogTableData[]>([]);

    useEffect(() => {
        api.get("/logs").then((res) => setLogs(res.data));

        const handleUpdate = (update: { logTable: LogTableData[], encryptedLogTable: EncryptedLogTableData[] }) => {
            setLogs(update.logTable);
            setEncryptedLogs(update.encryptedLogTable);
        };

        socket.on("updateData", handleUpdate);

        return () => {
            socket.off("updateData", handleUpdate);
        };
    }, []);

    const convertDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleString();
    };

    return (
        <Paper>
            <h2>Log Table</h2>
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
                    {logs.map((log) => (
                        <TableRow key={log.id}>
                            <TableCell>{log.id}</TableCell>
                            <TableCell>{log.main_id}</TableCell>
                            <TableCell>{log.action}</TableCell>
                            <TableCell>{log.action_date}</TableCell>
                        </TableRow>
                    ))}
                    {encryptedLogs.map((log) => (
                        <TableRow key={log.id}>
                            <TableCell>{log.id}</TableCell>
                            <TableCell>{log.main_id}</TableCell>
                            <TableCell>{log.encrypted_action}</TableCell>
                            <TableCell>{log.encrypted_action_date}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Paper>
    );
};

export default LogTable;
