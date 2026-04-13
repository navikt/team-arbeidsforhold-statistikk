import {ErrorMessage, List, Loader, Skeleton} from "@navikt/ds-react";
import {useEffect, useState} from "react";
import {DefaultApi} from "../api.ts";
import type {Repo} from "../api.ts";
import type {AxiosResponse} from "axios";

const api = new DefaultApi();


export default function RepoList() {
    const [repos, setRepos] = useState<Repo[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        api.meGet({})
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
            <List size="small">
                {repos.map((repo, index) => (
                    <List.Item key={index}>{repo.name}</List.Item>
                ))}
            </List>
        </>
    );
}
