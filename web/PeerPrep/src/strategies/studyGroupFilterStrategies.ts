import type { StudyGroup } from '../services/studyGroupService'

export type GroupFilterContext = {
  searchTerm: string
  subjectFilter: string
  dayFilter: string
  timeFilter: string
}

interface StudyGroupFilterStrategy {
  matches(group: StudyGroup, context: GroupFilterContext): boolean
}

class SearchFilterStrategy implements StudyGroupFilterStrategy {
  matches(group: StudyGroup, context: GroupFilterContext): boolean {
    const query = context.searchTerm.trim().toLowerCase()
    if (query.length === 0) {
      return true
    }

    return [group.subject, group.description, group.day, group.meetingTime, group.location, group.createdByName]
      .join(' ')
      .toLowerCase()
      .includes(query)
  }
}

class SubjectFilterStrategy implements StudyGroupFilterStrategy {
  matches(group: StudyGroup, context: GroupFilterContext): boolean {
    return context.subjectFilter === 'all' || group.subject.toLowerCase() === context.subjectFilter
  }
}

class DayFilterStrategy implements StudyGroupFilterStrategy {
  matches(group: StudyGroup, context: GroupFilterContext): boolean {
    return context.dayFilter === 'all' || group.day.toLowerCase() === context.dayFilter
  }
}

class TimeFilterStrategy implements StudyGroupFilterStrategy {
  matches(group: StudyGroup, context: GroupFilterContext): boolean {
    const normalizedTimeFilter = context.timeFilter.trim().toLowerCase()
    return normalizedTimeFilter.length === 0 || group.meetingTime.toLowerCase().includes(normalizedTimeFilter)
  }
}

const strategies: StudyGroupFilterStrategy[] = [
  new SearchFilterStrategy(),
  new SubjectFilterStrategy(),
  new DayFilterStrategy(),
  new TimeFilterStrategy(),
]

export function applyGroupFilters(groups: StudyGroup[], context: GroupFilterContext) {
  return groups.filter((group) => strategies.every((strategy) => strategy.matches(group, context)))
}
