import {Button, ErrorSummary} from "@navikt/ds-react";
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
    const [naisLoading, setNaisLoading] = useState(false);
    const [liquibaseLoading, setLiquibaseLoading] = useState(false);
    const handleCveClick = async (): Promise<void> => {
        setCveLoading(true);
        await api.apiAdminNvdjobbPost();
        setCveLoading(false);
    }
    const handleNaisClick = async (): Promise<void> => {
        setNaisLoading(true);
        await api.apiAdminNaisjobbPost();
        setNaisLoading(false);
    }
    const handleLiquibaseClick = async (): Promise<void> => {
        setLiquibaseLoading(true);
        await api.apiAdminLiquibaseUnlockPost();
        setLiquibaseLoading(false);
    }

    return (
        <>
            <ErrorSummary heading="Vær grei og ikke trykk på noen av disse knappene hvis du ikke vet nøyaktig hva du gjør.">
                <ErrorSummary.Item>Databasen kan komme i en tilstand hvor appen ikke fungerer.</ErrorSummary.Item>
                <ErrorSummary.Item>Eksterne leverandører kan blokkerer oss for å ha gjort for mange API-kall.</ErrorSummary.Item>
            </ErrorSummary>
            <Button data-color="danger" variant="primary" onClick={handleCveClick} loading={cveLoading}>
                Kjør CVE-jobb
            </Button>
            <Button data-color="danger" variant="primary" onClick={handleNaisClick} loading={naisLoading}>
                Oppdater data fra Nais
            </Button>
            <Button data-color="danger" variant="primary" onClick={handleLiquibaseClick} loading={liquibaseLoading}>
                Nullstill lås på liquibase
            </Button>
        </>
    );
}
