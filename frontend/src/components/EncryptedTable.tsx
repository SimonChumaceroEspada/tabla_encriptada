import { useState } from "react";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper, Button, TextField } from "@mui/material";
import { io, Socket } from "socket.io-client";

const socket: Socket = io("http://localhost:3003");

interface EncryptedLogTableData {
    id: string;
    main_id: string;
    encrypted_action: string;
    encrypted_action_date: string;
}

const EncryptedTable = () => {
    const [encryptedLogs, setEncryptedLogs] = useState<EncryptedLogTableData[]>([]);
    const [decryptedLogs, setDecryptedLogs] = useState<EncryptedLogTableData[]>([]);
    const [password, setPassword] = useState("");
    const [showDecrypted, setShowDecrypted] = useState(false);

    const handleUpdate = (update: { encryptedLogTable: EncryptedLogTableData[] }) => {
        setEncryptedLogs(update.encryptedLogTable);
    };

    const handleDecrypt = () => {
        // Simulate decryption process
        const decrypted = encryptedLogs.map(log => ({
            ...log,
            id: decrypt(log.id, password),
            main_id: decrypt(log.main_id, password),
            encrypted_action: decrypt(log.encrypted_action, password),
            encrypted_action_date: decrypt(log.encrypted_action_date, password)
        }));
        setDecryptedLogs(decrypted);
        setShowDecrypted(true);
    };

    const handleEncrypt = () => {
        setShowDecrypted(false);
    };

    const decrypt = (data: string, password: string) => {
        // Simulate decryption logic
        return data; // Replace with actual decryption logic
    };

    const convertDate = (dateString: string) => {
        // Convert date string to a valid date format
        return new Date(dateString.replace(/(\d{2})\/(\d{2})\/(\d{4})/, "$2/$1/$3"));
    };

    socket.on("updateData", handleUpdate);

    return (
        <Paper>
            <h2>Encrypted Log Table</h2>
            <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            <Button onClick={handleDecrypt}>Decrypt</Button>
            <Button onClick={handleEncrypt}>Encrypt</Button>
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
                    {(showDecrypted ? decryptedLogs : encryptedLogs).map((log) => (
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

export default EncryptedTable;
