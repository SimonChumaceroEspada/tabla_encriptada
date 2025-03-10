import MainTable from "./components/MainTable";
import LogTable from "./components/LogTable";
import EncryptedTable from "./components/EncryptedTable";
import { Container } from "@mui/material";

function App() {
   return (
       <Container>
           <h1>Database Viewer</h1>
           <MainTable />
           <LogTable />
           <EncryptedTable />
       </Container>
   );
}

export default App;
