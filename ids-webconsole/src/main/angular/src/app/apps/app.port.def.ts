export class PortDef {
    public readonly text: string;
    public readonly isLink: boolean;
    public readonly link?: string;

    constructor(text: string) {
        this.text = text;
        const matchedPort = /([0-9]+)->.*\/tcp$/.exec(text);
        this.isLink = matchedPort !== null;
        if (this.isLink) {
            this.link = 'http://localhost:' + matchedPort[1];
        }
    }
}
