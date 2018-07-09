import { mergeDeep, remove, set } from 'immutable';
import { pick } from 'lodash';
import { AppState, NativeModules } from 'react-native';
const RingModule = NativeModules.Ring as RingModule & { setup: (appGroup: string) => Promise<DataSet> };

interface RingModule {
  write: (version: number, data: string) => Promise<DataSet>;
  read: () => Promise<DataSet>;
}

export type RingListener = (key?: string, newValue?: any) => any;

export default {
  create: async (appGroup: string) => {
    const initialData = await RingModule.setup(appGroup);
    const ring = new Ring(initialData);
    return ring;
  },
};

export interface DataSet {
  version: number;
  data: string;
}

class Ring {

  private data: any;
  private dataString: string;
  private version: number;
  private changed = false;
  private listeners: { [ key: string ]: RingListener[] } = {
    change: [],
  };

  constructor({ version, data }: DataSet) {
    this.dataString = data;
    this.data = JSON.parse(data);
    this.version = version;
    AppState.addEventListener('change', async (status) => {
      if (status === 'active') await this.load();
      if (this.changed) {
        this.changed = false;
        this.emit('change');
      }
    });
  }

  public async setItem(key: string, value: any) {
    await this.save(set(this.data, key, value));
    return this;
  }

  public async getItem(key: string) {
    return this.data[ key ];
  }

  public async removeItem(key: string) {
    await this.save(remove(this.data, key));
    return this;
  }

  public async mergeItem(key: string, value: string) {
    await this.save(mergeDeep(this.data, { [ key ]: value }));
    return this;
  }

  public async clear() {
    await this.save({});
    return this;
  }

  public getAllKeys() {
    return Object.keys(this.data);
  }

  public async multiGet(keys: string[]) {
    return pick(this.data, keys);
  }

  public async multiSet(keyValuePairs: Array<[ string, any ]>) {
    let newBuffer: any;
    keyValuePairs.forEach((pair) => {
      newBuffer = !newBuffer ? set(this.data, pair[ 0 ], pair[ 1 ]) :
        set(newBuffer, pair[ 0 ], pair[ 1 ]);
    });
    await this.save(newBuffer);
    return this;
  }

  public async multiRemove(keys: string[]) {
    let newBuffer: any;
    keys.forEach((key) => {
      newBuffer = !newBuffer ? remove(this.data, key) :
        remove(newBuffer, key);
    });
    await this.save(newBuffer);
    return this;
  }

  public async multiMerge(keyValuePairs: Array<[ string, any ]>) {
    let newBuffer: any;
    keyValuePairs.forEach((pair) => {
      newBuffer = !newBuffer ? mergeDeep(this.data, { [ pair[ 0 ] ]: pair[ 1 ] }) :
        mergeDeep(newBuffer, { [ pair[ 0 ] ]: pair[ 1 ] });
    });
    await this.save(newBuffer);
    return this;
  }

  public removeAllListeners() {
    this.listeners = {
      change: [],
    };
  }

  public removeListener(eventType: 'change', listener: RingListener) {
    if (this.listeners[ eventType ]) {
      const removal = this.listeners[ eventType ].findIndex((l) => l === listener);
      if (removal !== -1) delete this.listeners[ eventType ][ removal ];
    }
  }

  public addListener(eventType: 'change', listener: RingListener) {
    this.listeners[ eventType ].push(listener);
  }

  private async save(data: any) {
    const result = await RingModule.write(this.version, JSON.stringify(data));
    this.dataString = result.data;
    this.data = JSON.parse(result.data);
    this.version = result.version;
  }

  private async load() {
    const result = await RingModule.read();
    if (this.dataString !== result.data) {
      this.dataString = result.data;
      this.changed = true;
      this.data = JSON.parse(result.data);
    }
    this.version = result.version;
  }

  private emit(eventType: 'change', key?: string, value?: any) {
    this.listeners[ eventType ].forEach((listener) => {
      listener(key, value);
    });
  }

}
