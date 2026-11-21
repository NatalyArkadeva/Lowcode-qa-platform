package ru.nataly.lowcode_qa_platform.domain.exception

class ProcessNotDeployedException(key: String) :
    RuntimeException("BPMN-схема '$key' не развёрнута в движке")
