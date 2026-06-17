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
    const [loading, setLoading] = useState(false);
    const handleClick = async (): Promise<void> => {
        setLoading(true);
        await api.apiAdminNvdjobbPost();
        setLoading(false);
    }

    return (
        <>
            <Button data-color="danger" variant="primary" onClick={handleClick} loading={loading}>
                Kjør CVE-jobb
            </Button>
        </>
    );
}
