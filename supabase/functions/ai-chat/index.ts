// ============================================================================
// PROMETHEUS COACH - AI CHAT EDGE FUNCTION
// ============================================================================
// Supabase Edge Function that proxies requests to Claude API
// Deploy: supabase functions deploy ai-chat
// Set secret: supabase secrets set ANTHROPIC_API_KEY=your-key-here
// ============================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
const MODEL = "claude-3-5-sonnet-20241022"  // or claude-3-haiku for faster/cheaper

// CORS headers for mobile app
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

// System prompts for different contexts
const SYSTEM_PROMPTS: Record<string, string> = {
  general: `You are an expert fitness and strength coach assistant for the Prometheus Coach app.
You help coaches with:
- Training program design and periodization
- Client communication strategies
- Exercise selection and technique cues
- Nutrition guidance principles
- Business and client management

Be concise, practical, and evidence-based. Use coaching terminology appropriately.
When giving advice, consider individual variation and the importance of progressive overload.
Format responses with clear structure using bullet points or numbered lists when appropriate.`,

  client_analysis: `You are analyzing a coaching client's training data and progress.
Provide actionable insights including:
- Progress assessment (what's working well)
- Areas needing attention
- Specific recommendations for the coach
- Potential red flags or concerns
- Suggested program modifications

Be specific and constructive. Reference training principles when making recommendations.`,

  program_design: `You are a program design specialist helping create training programs.
Consider these factors:
- Training age and experience level
- Available equipment and time
- Recovery capacity
- Specific goals (strength, hypertrophy, performance)
- Injury history and limitations

Provide structured programs with:
- Weekly split recommendations
- Exercise selection with alternatives
- Set/rep schemes with progression
- Rest period guidelines
- Deload recommendations`,

  message_draft: `You are helping a coach draft professional messages to their clients.
Messages should be:
- Warm but professional
- Encouraging and motivating
- Clear and actionable
- Appropriately brief

Maintain the coach's authority while being supportive.
Include specific references to the client's goals or progress when relevant.`,

  workout_review: `You are reviewing a workout design for a coach.
Evaluate and provide feedback on:
- Exercise selection and order
- Volume (sets x reps) appropriateness
- Intensity prescription
- Movement pattern balance
- Potential issues or improvements
- Alternative exercise suggestions

Be constructive and explain the reasoning behind suggestions.`
}

interface ChatRequest {
  message: string
  context_type: string
  conversation_history?: Array<{
    role: "user" | "assistant"
    content: string
  }>
  client_context?: {
    client_name?: string
    goals?: string[]
    recent_workouts?: number
    progress_summary?: string
  }
}

interface ClaudeMessage {
  role: "user" | "assistant"
  content: string
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const ANTHROPIC_API_KEY = Deno.env.get("ANTHROPIC_API_KEY")
    if (!ANTHROPIC_API_KEY) {
      throw new Error("ANTHROPIC_API_KEY not configured")
    }

    const {
      message,
      context_type,
      conversation_history = [],
      client_context
    }: ChatRequest = await req.json()

    if (!message) {
      throw new Error("Message is required")
    }

    // Build system prompt with context
    let systemPrompt = SYSTEM_PROMPTS[context_type] || SYSTEM_PROMPTS.general

    // Add client context if available
    if (client_context) {
      systemPrompt += "\n\n## Current Client Context:"
      if (client_context.client_name) {
        systemPrompt += `\nClient: ${client_context.client_name}`
      }
      if (client_context.goals?.length) {
        systemPrompt += `\nGoals: ${client_context.goals.join(", ")}`
      }
      if (client_context.recent_workouts !== undefined) {
        systemPrompt += `\nRecent workouts: ${client_context.recent_workouts}`
      }
      if (client_context.progress_summary) {
        systemPrompt += `\nProgress: ${client_context.progress_summary}`
      }
    }

    // Build messages array
    const messages: ClaudeMessage[] = [
      ...conversation_history.slice(-10), // Keep last 10 messages for context
      { role: "user", content: message }
    ]

    // Call Claude API
    const response = await fetch(ANTHROPIC_API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01"
      },
      body: JSON.stringify({
        model: MODEL,
        max_tokens: 1024,
        system: systemPrompt,
        messages: messages
      })
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error("Claude API error:", errorText)
      throw new Error(`Claude API error: ${response.status}`)
    }

    const claudeResponse = await response.json()

    // Extract the assistant's message
    const assistantMessage = claudeResponse.content?.[0]?.text || "I apologize, but I couldn't generate a response. Please try again."

    // Generate contextual suggestions
    const suggestions = generateSuggestions(context_type, message)

    return new Response(
      JSON.stringify({
        message: assistantMessage,
        suggestions: suggestions,
        usage: {
          input_tokens: claudeResponse.usage?.input_tokens,
          output_tokens: claudeResponse.usage?.output_tokens
        }
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200
      }
    )

  } catch (error) {
    console.error("Edge function error:", error)

    return new Response(
      JSON.stringify({
        error: error.message || "An error occurred",
        message: getFallbackResponse(error.message)
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 500
      }
    )
  }
})

// Generate contextual follow-up suggestions
function generateSuggestions(contextType: string, lastMessage: string): string[] {
  const suggestions: Record<string, string[]> = {
    general: [
      "Tell me more about this topic",
      "How do I implement this?",
      "What are common mistakes to avoid?"
    ],
    client_analysis: [
      "What specific exercises would help?",
      "How should I adjust their program?",
      "What should I discuss with them?"
    ],
    program_design: [
      "Can you provide alternatives?",
      "How should progression look?",
      "What about deload weeks?"
    ],
    message_draft: [
      "Make it more encouraging",
      "Add specific action items",
      "Make it shorter"
    ],
    workout_review: [
      "Suggest exercise swaps",
      "How to increase difficulty?",
      "What about recovery?"
    ]
  }

  return suggestions[contextType] || suggestions.general
}

// Fallback response when API fails
function getFallbackResponse(errorMessage: string): string {
  if (errorMessage?.includes("API_KEY")) {
    return "The AI service is not configured yet. Please contact support to enable this feature."
  }
  return "I'm temporarily unavailable. Please try again in a moment, or continue with your coaching expertise - you've got this!"
}
