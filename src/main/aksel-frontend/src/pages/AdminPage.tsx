import {Button} from "@navikt/ds-react";
import {AdminApi} from "../api.ts";
import axios from "axios";
import {useState} from "react";

const api = new AdminApi(
    undefined,
    undefined,
    axios.create({
        baseURL: window.location.origin,
    })
);

export default function AdminPage() {
    const [cveLoading, setCveLoading] = useState(false);
    const [liquibaseLoading, setLiquibaseLoading] = useState(false);
    const handleCveClick = async (): Promise<void> => {
        setCveLoading(true);
        await api.apiAdminNvdjobbPost();
        setCveLoading(false);
    }
    const handleLiquibaseClick = async (): Promise<void> => {
        setLiquibaseLoading(true);
        await api.apiAdminLiquibaseUnlockPost();
        setLiquibaseLoading(false);
    }

    return (
        <>
            <Button data-color="danger" variant="primary" onClick={handleCveClick} loading={cveLoading}>
                Kjør CVE-jobb
            </Button>
            <Button data-color="danger" variant="primary" onClick={handleLiquibaseClick} loading={liquibaseLoading}>
                Kjør CVE-jobb
            </Button>
        </>
    );
}
