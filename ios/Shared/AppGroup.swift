import Foundation

let kAppGroup = "group.com.nayal.typeshift"

var sharedDefaults: UserDefaults {
    UserDefaults(suiteName: kAppGroup) ?? .standard
}

extension UserDefaults {
    var groqAPIKey: String {
        get { string(forKey: "groq_api_key") ?? "" }
        set { set(newValue, forKey: "groq_api_key") }
    }
}
