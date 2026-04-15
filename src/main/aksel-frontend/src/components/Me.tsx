import {ErrorMessage, InternalHeader, Loader} from "@navikt/ds-react";

import {useEffect, useState} from "react";
import {MeApi} from "../api.ts";
import type {User} from "../api.ts";
import type {AxiosResponse} from "axios";

const api = new MeApi();

export function Me() {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        api.apiMeGet({})
            .then((response: AxiosResponse<User>) => {
                setUser(response.data);
            })
            .catch((err: unknown) => {
                setError("Failed to load user");
                console.error(err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    if (loading) return <Loader/>;
    if (error) return <ErrorMessage>{error}</ErrorMessage>;

    return (
        <InternalHeader.User name={user?.name}/>
    );
}
