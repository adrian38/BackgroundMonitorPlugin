export interface BackgroundMonitorPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
