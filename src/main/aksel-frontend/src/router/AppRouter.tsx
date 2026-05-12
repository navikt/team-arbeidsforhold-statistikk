import {BrowserRouter, Routes, Route} from "react-router-dom";
import FrontPage from "../pages/FrontPage.tsx";
import RepoList from "../pages/RepoList.tsx";

export function AppRouter() {
    return (
        <BrowserRouter>
            <Routes>
                <Route index element={<FrontPage/>}/>
                <Route path="/repoer" element={<RepoList/>}/>
            </Routes>
        </BrowserRouter>
    );
}
