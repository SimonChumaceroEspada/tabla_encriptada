import { useEffect, useState } from "react";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper } from "@mui/material";
import { io, Socket } from "socket.io-client";
import { api } from "../api";

const socket: Socket = io("http://localhost:3003");

interface MainTableData {
    id: number;
    name: string;
}

const MainTable = () => {
    const [data, setData] = useState<MainTableData[]>([]);

    useEffect(() => {
        // Cargar datos iniciales
        api.get("/data").then((res) => setData(res.data));

        // Escuchar actualizaciones en tiempo real
        const handleUpdate = (update: { mainTable: MainTableData[] }) => {
            setData(update.mainTable);
        };

        socket.on("updateData", handleUpdate);

        return () => {
            socket.off("updateData", handleUpdate);
        };
    }, []);

    return (
        <Paper>
            <h2>Main Table</h2>
            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Name</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row) => (
                        <TableRow key={row.id}>
                            <TableCell>{row.id}</TableCell>
                            <TableCell>{row.name}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Paper>
    );
};

export default MainTable;
