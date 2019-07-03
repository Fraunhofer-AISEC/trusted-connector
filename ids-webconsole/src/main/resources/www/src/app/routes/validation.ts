export class CounterExample {
    public explanation?: string;
    public steps?: Array<string>;
}

export class ValidationInfo {
    public valid = true;
    public counterExamples?: Array<CounterExample>;
}
