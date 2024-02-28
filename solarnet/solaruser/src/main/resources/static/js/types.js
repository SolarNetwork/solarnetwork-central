/**
 * A SettingSpecifier entity model.
 * 
 * @typedef {object} SettingSpecifier
 * @property {string} key the setting identifier
 * @property {boolean} direct flag if setting uses "direct" style input
 * @property {boolean} transient flag if setting is not persisted
 * @property {string} type the setting type
 */

/**
 * A ServiceInfo entity model.
 *
 * @typedef {object} ServiceInfo
 * @property {number} id the service identifier
 * @property {Array<SettingSpecifier>} settingSpecifiers the setting specifiers
 * @property {string} localizedName the display name
 * @property {string} localizedDescription brief description
 * @property {object} localizedInfoMessages mapping of setting specifier names and descriptions
 */
