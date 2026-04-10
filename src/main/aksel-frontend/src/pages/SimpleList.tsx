import { List, Heading } from "@navikt/ds-react";

const items = [
    "First item",
    "Second item",
    "Third item",
];

export default function SimpleList() {
    return (
        <>
            <Heading level="2" size="medium" spacing>
                Static list
            </Heading>

            <List size="small">
                {items.map((item, index) => (
                    <List.Item key={index}>{item}</List.Item>
                ))}
            </List>
        </>
    );
}
