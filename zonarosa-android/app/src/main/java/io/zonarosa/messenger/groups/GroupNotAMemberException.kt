package io.zonarosa.messenger.groups

class GroupNotAMemberException : GroupChangeException {
  constructor()
  constructor(throwable: Throwable) : super(throwable)
  constructor(throwable: GroupNotAMemberException) : super(throwable.cause ?: throwable)
}
