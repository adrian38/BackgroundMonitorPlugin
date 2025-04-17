import { WebPlugin } from '@capacitor/core';

import type { BackgroundMonitorPluginPlugin } from './definitions';

export class BackgroundMonitorPluginWeb extends WebPlugin implements BackgroundMonitorPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
