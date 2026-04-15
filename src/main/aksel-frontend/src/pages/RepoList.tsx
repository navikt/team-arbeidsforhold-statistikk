import {Accordion, BodyShort, ErrorMessage, List, Skeleton} from "@navikt/ds-react";
import {useEffect, useState} from "react";
import {RepoApi} from "../api.ts";
import type {Repo, RepoDetails} from "../api.ts";
import type {AxiosResponse} from "axios";
import axios from "axios";

const api = new RepoApi(
    undefined,
    undefined,
    axios.create({
        baseURL: window.location.origin,
    })
);

type RepoDetailsState = {
    loading: boolean;
    data?: RepoDetails;
    error?: string;
};

export default function RepoList() {
    const [repos, setRepos] = useState<Repo[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [repoDetails, setRepoDetails] = useState<Record<string, RepoDetailsState>>({});

    const loadRepoDetails = async (repoName: string) => {
        if (repoDetails[repoName]?.loading || repoDetails[repoName]?.data) {
            return;
        } else {
            setRepoDetails(old => ({
                ...old,
                [repoName]: {loading: true}
            }));

            try {
                const response = await api.apiRepoRepoNameGet({name: repoName});
                setRepoDetails(old => ({
                    ...old,
                    [repoName]: {loading: false, data: response.data}
                }));
            } catch (e) {
                setRepoDetails(old => ({
                    ...old,
                    [repoName]: {
                        loading: false,
                        error: "Kunne ikke hente data"
                    }
                }));
            }

        }
    };

    useEffect(() => {
        api.apiRepoGet({})
            .then((response: AxiosResponse<Repo[]>) => {
                setRepos(response.data);
            })
            .catch((err: unknown) => {
                setError("Failed to load repos");
                console.error(err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    if (loading) return (
        <>
            <Skeleton>
                <List size={"small"}>
                    <List.Item key="1">navikt/something</List.Item>
                    <List.Item key="2">navikt/something</List.Item>
                    <List.Item key="3">navikt/something</List.Item>
                </List>
            </Skeleton>
        </>
    );
    if (error) return <ErrorMessage>{error}</ErrorMessage>;

    return (
        <>
            <Accordion>
                {repos.map((repo) => {
                    const expandedRepo = repoDetails[repo.name];
                    return (
                        <Accordion.Item key={repo.name} onOpenChange={(open) => {
                            if (open) {
                                loadRepoDetails(repo.name);
                            }
                        }}>
                            <Accordion.Header>
                                {repo.name}
                            </Accordion.Header>
                            <Accordion.Content>
                                {!expandedRepo || expandedRepo.loading ? (
                                    <Skeleton>
                                        <BodyShort>Lorem ipsum</BodyShort>
                                    </Skeleton>
                                ) : expandedRepo.error ? (
                                        <ErrorMessage>{expandedRepo.error}</ErrorMessage>
                                    )
                                    : (
                                        <>
                                            <BodyShort>
                                                Miljøer: {expandedRepo.data?.environments}
                                            </BodyShort>
                                        </>
                                    )}
                            </Accordion.Content>
                        </Accordion.Item>
                    )
                })
                }
            </Accordion>
        </>
    );
}
