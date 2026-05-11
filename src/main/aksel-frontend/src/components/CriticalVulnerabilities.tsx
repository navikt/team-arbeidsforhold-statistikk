import {BodyShort, ErrorMessage, Heading, HStack, Loader, Tag, VStack} from "@navikt/ds-react";

import {useEffect, useState} from "react";
import {VulnerabilityApi} from "../api.ts";
import type {CriticalVulnerability} from "../api.ts";
import axios, {type AxiosResponse} from "axios";

const api = new VulnerabilityApi(
    undefined,
    undefined,
    axios.create({
        baseURL: window.location.origin,
    })
);

export function CriticalVulnerabilities() {
    const [vulnerabilities, setVulnerabilities] = useState<CriticalVulnerability[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        api.apiVulnerabilityCriticalGet()
            .then((response: AxiosResponse<CriticalVulnerability[]>) => {
                setVulnerabilities(response.data);
            })
            .catch((err: unknown) => {
                setError("Failed to load critical vulnerabilities");
                console.error(err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    if (loading) return <Loader/>;
    if (error) return <ErrorMessage>{error}</ErrorMessage>;

    if(vulnerabilities.length == 0) return (
        <VStack>
            <Heading level="2" size="small">
                Kritiske sårbarheter
            </Heading>

            <HStack align="center">
                <Tag variant="success" size="xsmall">
                    Ingen sårbarheter!
                </Tag>
                <BodyShort>
                    Ingen nye kritiske sårbarheter ble funnet
                </BodyShort>
            </HStack>

            <BodyShort textColor="subtle">
                Godt jobba!
            </BodyShort>
        </VStack>
    );

    return (
        <VStack>
            <Heading size="small" level="2">
                Kritiske sårbarheter
            </Heading>

            {vulnerabilities.map((vuln) => (
                <HStack key={vuln.id} align="center">
                    <Tag variant="error" size="xsmall">
                        Kritisk sårbarhet
                    </Tag>
                    <BodyShort>
                        {vuln.id}
                    </BodyShort>
                    <BodyShort textColor="subtle">
                        I {vuln.affectedRepositories} repoer
                    </BodyShort>
                </HStack>
            ))}

        </VStack>
    );
}
