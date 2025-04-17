import { registerPlugin } from '@capacitor/core';

import type { BackgroundMonitorPluginPlugin } from './definitions';

const BackgroundMonitorPlugin = registerPlugin<BackgroundMonitorPluginPlugin>('BackgroundMonitorPlugin', {
  web: () => import('./web').then((m) => new m.BackgroundMonitorPluginWeb()),
});

export * from './definitions';
export { BackgroundMonitorPlugin };
