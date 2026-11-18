import {Link} from "@navikt/ds-react";
import {CriticalVulnerabilities} from "../components/CriticalVulnerabilities.tsx";

export default function FrontPage() {
    return (
        <>
            <Link href="/ingresser">Ingresser etter eksponering</Link>
            <CriticalVulnerabilities/>
        </>
    );
}
