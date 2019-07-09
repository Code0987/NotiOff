import { Signale } from "signale";
export interface ILastRelease {
    version: string;
    gitTag: string;
    gitHead: string;
}
export interface INextRelease extends ILastRelease {
    notes: string;
}
/**
 * The context object defined in semantic-release/index.js
 * @see https://github.com/semantic-release/semantic-release/blob/v15.13.3/index.js
 */
export interface IContext {
    cwd: string;
    env: object;
    stdout: WritableStream;
    stderr: WritableStream;
    options: object;
    nextRelease: INextRelease;
    logger: Signale;
}
