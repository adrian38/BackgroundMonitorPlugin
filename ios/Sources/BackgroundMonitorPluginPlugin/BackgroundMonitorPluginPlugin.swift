import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(BackgroundMonitorPluginPlugin)
public class BackgroundMonitorPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "BackgroundMonitorPluginPlugin"
    public let jsName = "BackgroundMonitorPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = BackgroundMonitorPlugin()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo(value)
        ])
    }
}
