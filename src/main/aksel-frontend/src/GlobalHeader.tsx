import {Spacer, InternalHeader} from "@navikt/ds-react";

export function GlobalHeader() {
    return (
        <header>
            <InternalHeader>
                <InternalHeader.Title as="h1">Team Arbeidsforhold's repoer</InternalHeader.Title>
                <Spacer/>
                <InternalHeader.User name="Ola Normann"/>
            </InternalHeader>
        </header>
    );
}