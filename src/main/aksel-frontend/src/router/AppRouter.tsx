import {BrowserRouter, Routes, Route} from "react-router-dom";
import SimpleList from "../pages/SimpleList.tsx";

export function AppRouter() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="simplelist/*" element={<SimpleList/>}/>
            </Routes>
        </BrowserRouter>
    );
}
