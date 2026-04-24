import { useTranslation } from 'react-i18next';

export default function LanguageSelector() {
    const { i18n } = useTranslation();

    const changeLanguage = (lng) => {
        i18n.changeLanguage(lng);
    };

    return (
        <div className="language-selector-bar">
            <select
                className="language-selector-select"
                value={i18n.language}
                onChange={(e) => changeLanguage(e.target.value)}
            >
                <option value="en">EN</option>
                <option value="tr">TR</option>
                <option value="es">ES</option>
                <option value="zh">ZH</option>
            </select>
        </div>
    );
}