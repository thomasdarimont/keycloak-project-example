<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Context Selection
    <#elseif section = "header">
        Context selection
    <#elseif section = "form">
        <p>Please select a Context:</p>
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-context-selection-form"
                      method="post">

                    <datalist id="contextOptions">
                        <#list contextOptions as contextOption>
                            <option data-value="${contextOption.key!}" value="${contextOption.label!}"></option>
                        </#list>
                    </datalist>

                    <script defer>

                        let contextKeyElement = document.getElementById("contextKey");

                        function restrictInputToAllowedOptions(inputElement) {
                            if (inputElement.value === "") {
                                return;
                            }
                            var options = inputElement.list.options;
                            for (var i = 0; i < options.length; i++) {
                                let option = options[i];
                                if (inputElement.value === option.value) {
                                    // use context key from data-value element of current option
                                    let contextKeyElement = document.getElementById("contextKey");
                                    contextKeyElement.value = option.dataset.value;
                                    return;
                                }
                            }
                            //no match was found: reset the value
                            inputElement.value = "";
                        }
                    </script>

                    <div class="${properties.kcFormGroupClass!}">
                        <label for="context" class="${properties.kcLabelClass!}">Context</label>
                        <input id="context" list="contextOptions"
                               class="${properties.kcInputClass!}"
                               <#if currentContext??>placeholder="${currentContext.label!''}"</#if>
                               onchange="restrictInputToAllowedOptions(this);" required/>
                        <input id="contextKey" name="context.selection.key"
                               <#if currentContext??>value="${currentContext.value!''}"</#if>
                               type="hidden"/>
                    </div>

                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" value="${msg("doSubmit")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                               name="cancel" id="kc-cancel" type="submit" value="${msg("doCancel")}" formnovalidate="formnovalidate"/>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
