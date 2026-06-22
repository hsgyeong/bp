import http from './http'

export function drawGulbiReward(weeklyBudgetId, body) {
    return http.post(`/budgets/weekly/${weeklyBudgetId}/gulbi-reward/draw`, body)
}

export function decideGulbiReward(weeklyBudgetId, decision) {
    return http.post(`/budgets/weekly/${weeklyBudgetId}/gulbi-reward/decision`, { decision })
}

export function getGulbiReward(weeklyBudgetId) {
    return http.get(`/budgets/weekly/${weeklyBudgetId}/gulbi-reward`)
}