import { List } from "@navikt/ds-react";
import {CriticalVulnerabilities} from "../components/CriticalVulnerabilities.tsx";

const items = [
    "First item",
    "Second item",
    "Third item",
];

export default function FrontPage() {
    return (
        <>
            <CriticalVulnerabilities/>

            <List size="small">
                {items.map((item, index) => (
                    <List.Item key={index}>{item}</List.Item>
                ))}
            </List>
        </>
    );
}
