function accountManagement() {
	'use strict';

	const linkModal = $('#account-link-modal');
	const unlinkModal = $('#account-unlink-modal');

	/**
	 * Show an error message at the top of a modal's body.
	 *
	 * @param {jQuery} modal the modal form
	 * @param {String} msg the message to show, as plain text
	 */
	function showModalError(modal, msg) {
		modal.find('.modal-body .alert').remove();
		SolarReg.showAlertBefore(modal.find('.modal-body > *:first-child'), 'alert-warning',
			$('<div>').text(msg).html());
	}

	/**
	 * Submit a modal form with AJAX, and reload the page on success to show the changes.
	 *
	 * @returns {boolean} false, to prevent the default form submission
	 */
	function modalFormSubmit() {
		const modal = $(this);
		const submitBtn = modal.find('button[type=submit]');
		modal.find('.modal-body .alert').remove();
		submitBtn.prop('disabled', true);
		modal.ajaxSubmit({
			dataType: 'json',
			success: function(json) {
				if ( json && json.success === true ) {
					document.location.reload();
				} else {
					submitBtn.prop('disabled', false);
					showModalError(modal, SolarReg.formatResponseMessage(json) || 'Unknown error.');
				}
			},
			error: function(xhr, statusText, error) {
				submitBtn.prop('disabled', false);
				showModalError(modal, SolarReg.extractResponseMessage(xhr, statusText, error));
			}
		});
		return false;
	}

	/**
	 * Reset a modal form after it is hidden.
	 */
	function modalFormReset() {
		const modal = $(this);
		modal.find('.modal-body .alert').remove();
		modal.find('button[type=submit]').prop('disabled', false);
		this.reset();
	}

	// ***** Link account
	linkModal
		.on('submit', modalFormSubmit)
		.on('shown.bs.modal', function() {
			linkModal.find('input[name=username]').trigger('focus');
		})
		.on('hidden.bs.modal', modalFormReset)
		;

	// ***** Unlink account
	unlinkModal
		.on('submit', modalFormSubmit)
		.on('hidden.bs.modal', modalFormReset)
		;

	$('.consolidated-child-unlink').on('click', function() {
		const form = unlinkModal.get(0);
		const btn = this;

		form.elements['userId'].value = btn.dataset.userId;
		SolarReg.Templates.replaceTemplateProperties(unlinkModal.find('.modal-body'), {
			userName: btn.dataset.userName || '',
			userEmail: btn.dataset.userEmail || ''
		});
		unlinkModal.modal('show');
	});
}
$(document).ready(() => {
	$('#account-management').first().each(accountManagement);
});
