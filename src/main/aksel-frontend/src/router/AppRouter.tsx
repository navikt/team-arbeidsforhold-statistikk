import {BrowserRouter, Routes, Route} from "react-router-dom";
import SimpleList from "../pages/SimpleList.tsx";
import RepoList from "../pages/RepoList.tsx";

export function AppRouter() {
    return (
        <BrowserRouter>
            <Routes>
                <Route index element={<SimpleList/>}/>
                <Route path="/repoer" element={<RepoList/>}/>
            </Routes>
        </BrowserRouter>
    );
}
