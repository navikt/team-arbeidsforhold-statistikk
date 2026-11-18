import {BrowserRouter, Routes, Route} from "react-router-dom";
import FrontPage from "../pages/FrontPage.tsx";
import RepoList from "../pages/RepoList.tsx";
import AdminPage from "../pages/AdminPage.tsx";
import IngressList from "../pages/IngressList.tsx";

export function AppRouter() {
    return (
        <BrowserRouter>
            <Routes>
                <Route index element={<FrontPage/>}/>
                <Route path="/repoer" element={<RepoList/>}/>
                <Route path="/ingresser" element={<IngressList/>}/>
                <Route path="/admin" element={<AdminPage/>}/>
            </Routes>
        </BrowserRouter>
    );
}
