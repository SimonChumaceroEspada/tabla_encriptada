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

const LogTable = () => {
    const [logs, setLogs] = useState<LogTableData[]>([]);

    useEffect(() => {
        api.get("/logs").then((res) => setLogs(res.data));

        const handleUpdate = (update: { logTable: LogTableData[] }) => {
            setLogs(update.logTable);
        };

        socket.on("updateData", handleUpdate);

        return () => {
            socket.off("updateData", handleUpdate);
        };
    }, []);

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
                            <TableCell>{new Date(log.action_date).toLocaleString()}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Paper>
    );
};

export default LogTable;
