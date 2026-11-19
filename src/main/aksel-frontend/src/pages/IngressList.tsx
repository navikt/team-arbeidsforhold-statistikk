import {BodyShort, Button, ErrorMessage, Heading, Link, Loader, Table, VStack} from "@navikt/ds-react";
import {useEffect, useState} from "react";
import axios from "axios";
import {IngressApi, type IngressOverview} from "../api.ts";
import "./IngressList.css";

const api = new IngressApi(undefined, undefined, axios.create({baseURL: window.location.origin}));
const tidspunkt = new Intl.DateTimeFormat("nb-NO", {dateStyle: "short", timeStyle: "short"});

export default function IngressList() {
    const [oversikt, setOversikt] = useState<IngressOverview>();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    const [forsøk, setForsøk] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        api.apiIngressGet({signal: controller.signal})
            .then(response => setOversikt(response.data))
            .catch(() => {
                if (!controller.signal.aborted) {
                    setError(true);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [forsøk]);

    return (
        <VStack as="main" gap="space-24" padding={{xs: "space-16", md: "space-32"}} className="ingress-list">
            <Link href="/">Til forsiden</Link>
            <Heading level="2" size="large">Ingresser etter eksponering</Heading>
            <BodyShort>
		Apper med ingresser, sortert etter hvor åpent tilgjengelig de er (basert på domene, tilgangskontroll er ikke vurdert)
            </BodyShort>
            {loading ? (
                <Loader title="Henter ingresser"/>
            ) : error ? (
                <VStack gap="space-16" align="start">
                    <ErrorMessage role="alert">Kunne ikke hente ingressoversikten. Prøv igjen.</ErrorMessage>
                    <Button variant="secondary" onClick={() => {
                        setError(false);
                        setLoading(true);
                        setForsøk(forsøk + 1);
                    }}>Prøv igjen</Button>
                </VStack>
            ) : oversikt && (
                <>
                    {oversikt.deploymentsWithoutManifest > 0 && (
                        <BodyShort role="status">
                            {oversikt.deploymentsWithoutManifest} deployment mangler lagret manifest og kan ikke vurderes.
                        </BodyShort>
                    )}
                    {oversikt.groups.length === 0 && (
                        <BodyShort>Ingen ingresser funnet i de lagrede manifestene.</BodyShort>
                    )}
                    {oversikt.groups.map(gruppe => (
                        <VStack as="section" gap="space-8" key={gruppe.exposure}
                                aria-labelledby={`ingress-${gruppe.exposure}`}>
                            <Heading level="3" size="medium" id={`ingress-${gruppe.exposure}`}>
                                {gruppe.name} ({gruppe.ingresses.length})
                            </Heading>
                            <BodyShort>{gruppe.description}</BodyShort>
                            <div className="ingress-list__table" role="region"
                                 aria-label={`Ingresser: ${gruppe.name}`} tabIndex={0}>
                                <Table size="small" aria-labelledby={`ingress-${gruppe.exposure}`}>
                                    <Table.Header>
                                        <Table.Row>
                                            <Table.HeaderCell scope="col">Ingress</Table.HeaderCell>
                                            <Table.HeaderCell scope="col">Repo</Table.HeaderCell>
                                            <Table.HeaderCell scope="col">App</Table.HeaderCell>
                                            <Table.HeaderCell scope="col">Miljø</Table.HeaderCell>
                                            <Table.HeaderCell scope="col">Sist observert</Table.HeaderCell>
                                        </Table.Row>
                                    </Table.Header>
                                    <Table.Body>
                                        {gruppe.ingresses.map(ingress => (
                                            <Table.Row key={JSON.stringify([
                                                ingress.url, ingress.workload, ingress.environment, ingress.suffix
                                            ])}>
                                                <Table.DataCell>{ingress.url}</Table.DataCell>
                                                <Table.DataCell>
                                                    {ingress.repository ? (
                                                        <Link href={`https://github.com/${ingress.repository}`}>
                                                            {ingress.repository}
                                                        </Link>
                                                    ) : "Ukjent repo"}
                                                </Table.DataCell>
                                                <Table.DataCell>{ingress.workload}</Table.DataCell>
                                                <Table.DataCell>
                                                    {ingress.environment}{ingress.suffix && ` (${ingress.suffix})`}
                                                </Table.DataCell>
                                                <Table.DataCell>
                                                    {ingress.lastSeen ? (
                                                        <time dateTime={ingress.lastSeen}>
                                                            {tidspunkt.format(new Date(ingress.lastSeen))}
                                                        </time>
                                                    ) : "Ukjent"}
                                                </Table.DataCell>
                                            </Table.Row>
                                        ))}
                                    </Table.Body>
                                </Table>
                            </div>
                        </VStack>
                    ))}
                </>
            )}
        </VStack>
    );
}
