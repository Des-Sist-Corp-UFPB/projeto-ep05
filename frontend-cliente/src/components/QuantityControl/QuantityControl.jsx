

function QuantityControl({min = 0, max = 10, value, onChange}){
    const decrease = () => {
        if (value > min ) onChange(value - 1)
    }
    const increase = () =>{
        if (value < max) onChange(value + 1)
    }

    return (
        <div className="quantity-control">
            <button onClick={decrease} disabled={value === min}>-</button>
            <span>{value}</span>
            <button onClick={increase} disabled={value === max}>+</button>
        </div>
    )
}

export default QuantityControl;