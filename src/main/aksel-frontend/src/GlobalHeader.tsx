import {Spacer, InternalHeader} from "@navikt/ds-react";
import {Me} from "./components/Me.tsx"

export function GlobalHeader() {
    return (
        <header>
            <InternalHeader>
                <InternalHeader.Title as="h1">Team Arbeidsforhold's repoer</InternalHeader.Title>
                <Spacer/>
                <Me/>
            </InternalHeader>
        </header>
    );
}