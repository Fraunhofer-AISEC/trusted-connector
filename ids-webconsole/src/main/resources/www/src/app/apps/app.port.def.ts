export class PortDef {
    readonly text: string;
    readonly isLink: boolean;
    readonly link?: string;

    constructor(text: string) {
        this.text = text;
        const matchedPort = /([0-9]+)->.*\/tcp$/.exec(text);
        this.isLink = matchedPort !== null;
        if (this.isLink) {
            this.link = 'http://localhost:' + matchedPort[1];
        }
    }
}
