import {BodyShort} from "@navikt/ds-react";
import {CriticalVulnerabilities} from "../components/CriticalVulnerabilities.tsx";

export default function FrontPage() {
    return (
        <>
            <CriticalVulnerabilities/>

            <BodyShort>Denne siden er under konstruksjon, foreløpig er det kun widget-en over med nylige kritiske sårbarheter.</BodyShort>
        </>
    );
}
