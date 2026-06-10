import Foundation

enum APIError: LocalizedError {
    case noKey, badStatus(Int), parseFailure

    var errorDescription: String? {
        switch self {
        case .noKey:           return "No API key set"
        case .badStatus(let c): return "Server error \(c)"
        case .parseFailure:    return "Unexpected response"
        }
    }
}

func callGroq(text: String, instruction: String, apiKey: String) async throws -> String {
    guard !apiKey.isEmpty else { throw APIError.noKey }

    let url = URL(string: "https://api.groq.com/openai/v1/chat/completions")!
    var req = URLRequest(url: url)
    req.httpMethod = "POST"
    req.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
    req.setValue("application/json",  forHTTPHeaderField: "Content-Type")
    req.timeoutInterval = 20

    let body: [String: Any] = [
        "model": "llama-3.3-70b-versatile",
        "messages": [
            ["role": "system", "content": instruction],
            ["role": "user",   "content": text]
        ],
        "max_tokens": 2048,
        "temperature": 0.7
    ]
    req.httpBody = try JSONSerialization.data(withJSONObject: body)

    let (data, response) = try await URLSession.shared.data(for: req)

    if let http = response as? HTTPURLResponse, http.statusCode != 200 {
        throw APIError.badStatus(http.statusCode)
    }

    guard
        let json     = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
        let choices  = json["choices"]  as? [[String: Any]],
        let message  = choices.first?["message"] as? [String: Any],
        let content  = message["content"] as? String
    else { throw APIError.parseFailure }

    return content.trimmingCharacters(in: .whitespacesAndNewlines)
}
