itemRow(item) ::= <<
<tr>
	<th>$item.planName$</th>
	<td>$messages.([item.planName,".item"])$</td>
	<td>$invoice.invoiceNumber$</td>
</tr>
>>
